from flask import Flask, render_template, request, jsonify
from flask_cors import CORS
import json
import os
import time
import requests
from datetime import datetime

app = Flask(__name__)
CORS(app)  # permite CORS para chamadas do frontend em localhost

# ─────────────────────────────────────────────────────────────────
# Ficheiros de dados
# ─────────────────────────────────────────────────────────────────
DATA_FILE     = 'reviews.json'
EPISODES_FILE = 'episodes.json'   # notas de episódios por utilizador

# ─────────────────────────────────────────────────────────────────
# Cache em memória — evita flood na Kitsu API
# Estrutura: { key: { 'data': ..., 'ts': timestamp } }
# TTL: 30 minutos (1800 s)
# ─────────────────────────────────────────────────────────────────
_cache  = {}
CACHE_TTL = 1800  # segundos

KITSU_BASE    = 'https://kitsu.io/api/edge'
KITSU_HEADERS = {
    'Accept':     'application/vnd.api+json',
    'User-Agent': 'AniReview/1.0 (flask-backend)',
}


def cache_get(key):
    """Retorna dado do cache se ainda válido, senão None."""
    entry = _cache.get(key)
    if entry and (time.time() - entry['ts']) < CACHE_TTL:
        return entry['data']
    return None


def cache_set(key, data):
    """Guarda dado no cache com timestamp atual."""
    _cache[key] = {'data': data, 'ts': time.time()}


# ─────────────────────────────────────────────────────────────────
# Helpers de I/O
# ─────────────────────────────────────────────────────────────────

def load_json(path, default=None):
    if default is None:
        default = []
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            try:
                return json.load(f)
            except json.JSONDecodeError:
                return default
    return default


def save_json(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


# ─────────────────────────────────────────────────────────────────
# Kitsu helpers
# ─────────────────────────────────────────────────────────────────

def kitsu_get_relationships(anime_id):
    """
    Busca as relações de um anime via Kitsu media-relationships endpoint.
    Retorna { 'sequels': [id, ...], 'prequels': [id, ...] }
    Endpoint: GET /media-relationships?filter[source_id]=<id>&filter[source_type]=Anime&include=destination
    """
    cache_key = f'rels:{anime_id}'
    cached = cache_get(cache_key)
    if cached is not None:
        return cached

    result = {'sequels': [], 'prequels': []}

    try:
        resp = requests.get(
            f'{KITSU_BASE}/media-relationships',
            params={
                'filter[source_id]':   anime_id,
                'filter[source_type]': 'Anime',
                'include':             'destination',
                'page[limit]':         20,
            },
            headers=KITSU_HEADERS,
            timeout=8,
        )
        if not resp.ok:
            cache_set(cache_key, result)
            return result

        body = resp.json()

        # included → mapa id → destinationId
        included = {item['id']: item for item in body.get('included', [])}

        for rel in body.get('data', []):
            attrs = rel.get('attributes', {})
            role  = (attrs.get('role') or '').lower()

            dest_data = rel.get('relationships', {}).get('destination', {}).get('data')
            if not dest_data:
                continue

            dest_id = str(dest_data.get('id', ''))
            dest_type = dest_data.get('type', '')

            # Só queremos relações com outros Animes
            if dest_type not in ('anime', 'Anime'):
                continue

            if role == 'sequel':
                result['sequels'].append(dest_id)
            elif role == 'prequel':
                result['prequels'].append(dest_id)

    except Exception as e:
        app.logger.warning(f'kitsu_get_relationships({anime_id}) falhou: {e}')

    cache_set(cache_key, result)
    return result


def kitsu_get_anime_info(anime_id):
    """Busca atributos básicos de um anime via Kitsu API com cache."""
    cache_key = f'anime:{anime_id}'
    cached = cache_get(cache_key)
    if cached is not None:
        return cached

    try:
        resp = requests.get(
            f'{KITSU_BASE}/anime/{anime_id}',
            headers=KITSU_HEADERS,
            timeout=8,
        )
        if not resp.ok:
            return None

        data   = resp.json().get('data', {})
        attrs  = data.get('attributes', {})
        result = {
            'id':           str(data.get('id', anime_id)),
            'title':        attrs.get('canonicalTitle', ''),
            'episodeCount': attrs.get('episodeCount'),
            'startDate':    attrs.get('startDate'),
            'subtype':      attrs.get('subtype'),
        }
        cache_set(cache_key, result)
        return result

    except Exception as e:
        app.logger.warning(f'kitsu_get_anime_info({anime_id}) falhou: {e}')
        return None


# ─────────────────────────────────────────────────────────────────
# Rotas principais
# ─────────────────────────────────────────────────────────────────

@app.route('/')
def index():
    return render_template('index.html')


# ─────────────────────────────────────────────────────────────────
# GET /api/animes/<kitsuId>/seasons
#
# Retorna a lista real de temporadas do anime.
# Estratégia:
#   1. Navega prequels para encontrar o root (T1)
#   2. Percorre sequels a partir do root para montar a chain
#   3. Busca detalhes de cada entrada e monta response
# ─────────────────────────────────────────────────────────────────

@app.route('/api/animes/<kitsu_id>/seasons', methods=['GET'])
def get_seasons(kitsu_id):
    kitsu_id  = str(kitsu_id)
    cache_key = f'seasons:{kitsu_id}'

    cached = cache_get(cache_key)
    if cached is not None:
        return jsonify(cached)

    try:
        # ── Passo 1: Navegar até a T1 (root sem prequel) ──
        root_id = kitsu_id
        visited = set()

        for _ in range(8):
            if root_id in visited:
                break
            visited.add(root_id)

            rels     = kitsu_get_relationships(root_id)
            prequels = rels.get('prequels', [])
            if not prequels:
                break
            root_id = str(prequels[0])

        # ── Passo 2: Percorrer sequels a partir do root ──
        chain  = [root_id]
        seen   = {root_id}
        cursor = root_id

        for _ in range(10):
            rels    = kitsu_get_relationships(cursor)
            sequels = [x for x in rels.get('sequels', []) if x not in seen]
            if not sequels:
                break
            cursor = sequels[0]
            seen.add(cursor)
            chain.append(cursor)

        # ── Passo 3: Buscar detalhes e montar response ──
        ordinals = ['1ª', '2ª', '3ª', '4ª', '5ª', '6ª', '7ª', '8ª', '9ª', '10ª']
        seasons  = []

        for idx, kid in enumerate(chain):
            info  = kitsu_get_anime_info(kid) or {}
            label = f'{ordinals[idx] if idx < len(ordinals) else str(idx + 1) + "ª"} Temporada'

            seasons.append({
                'id':           f'{kid}-s{idx + 1}',
                'nome':         label,
                'label':        label,
                'kitsuId':      kid,
                'title':        info.get('title', ''),
                'episodeCount': info.get('episodeCount'),
                'startDate':    info.get('startDate'),
            })

        # Série única → label mais descritivo
        if len(seasons) == 1:
            seasons[0]['nome']  = 'Série Completa'
            seasons[0]['label'] = 'Série Completa'

        # Guarda no cache para todos os IDs da chain
        for kid in chain:
            cache_set(f'seasons:{kid}', seasons)

        return jsonify(seasons)

    except requests.Timeout:
        return jsonify({'error': 'Kitsu API não respondeu a tempo'}), 504
    except Exception as e:
        app.logger.error(f'Erro em get_seasons({kitsu_id}): {e}')
        return jsonify({'error': str(e)}), 500


# ─────────────────────────────────────────────────────────────────
# GET /api/seasons/<seasonId>/episodes
#
# seasonId formato: "<kitsuId>-s<n>" (ex.: "7442-s1")
# Query param opcional: ?userName=<nome>
# Retorna episódios da Kitsu com notas locais do utilizador
# ─────────────────────────────────────────────────────────────────

@app.route('/api/seasons/<season_id>/episodes', methods=['GET'])
def get_episodes(season_id):
    user_name = request.args.get('userName', '').strip()

    # Extrai o kitsuId do seasonId (ex.: "7442-s1" → "7442")
    kitsu_id = season_id.split('-s')[0] if '-s' in season_id else season_id

    cache_key = f'episodes:{kitsu_id}'
    cached_eps = cache_get(cache_key)

    if cached_eps is None:
        # Busca episódios na Kitsu API (paginação: até 20/página, max 5 páginas)
        all_episodes = []
        page_offset  = 0
        page_limit   = 20

        for _ in range(5):
            try:
                resp = requests.get(
                    f'{KITSU_BASE}/anime/{kitsu_id}/episodes',
                    params={
                        'page[limit]':  page_limit,
                        'page[offset]': page_offset,
                        'sort':         'number',
                    },
                    headers=KITSU_HEADERS,
                    timeout=10,
                )
                if not resp.ok:
                    break

                data  = resp.json()
                items = data.get('data', [])
                if not items:
                    break

                for ep in items:
                    a      = ep.get('attributes', {})
                    titles = a.get('titles', {})
                    # Prioridade: PT-BR → EN → canonical
                    title  = (
                        titles.get('pt_br') or
                        titles.get('en')    or
                        titles.get('en_jp') or
                        a.get('canonicalTitle', '')
                    ) or ''

                    all_episodes.append({
                        'kitsuEpId': ep['id'],
                        'numero':    a.get('number') or (len(all_episodes) + 1),
                        'titulo':    title,
                        'airdate':   a.get('airdate'),
                        'length':    a.get('length'),
                    })

                # Próxima página?
                if not data.get('links', {}).get('next'):
                    break
                page_offset += page_limit

            except requests.Timeout:
                break
            except Exception as e:
                app.logger.error(f'Erro ao buscar eps Kitsu {kitsu_id} offset {page_offset}: {e}')
                break

        cached_eps = all_episodes
        if cached_eps:
            cache_set(cache_key, cached_eps)

    # Carrega notas salvas localmente
    all_ratings = load_json(EPISODES_FILE, default={})

    # Monta resposta enriquecida
    result = []
    for ep in cached_eps:
        ep_key     = f'{kitsu_id}-{ep["numero"]}'
        ep_ratings = all_ratings.get(ep_key, {})

        notas_vals  = [v for v in ep_ratings.values() if isinstance(v, (int, float))]
        nota_media  = (sum(notas_vals) / len(notas_vals)) if notas_vals else 0.0
        total_notas = len(notas_vals)
        nota_user   = ep_ratings.get(user_name) if user_name else None

        result.append({
            'id':          ep_key,
            'animeId':     kitsu_id,
            'seasonId':    season_id,
            'numero':      ep['numero'],
            'titulo':      ep.get('titulo', ''),
            'airdate':     ep.get('airdate'),
            'length':      ep.get('length'),
            'notaUsuario': nota_user,
            'notaMedia':   round(nota_media, 2),
            'totalNotas':  total_notas,
        })

    return jsonify(result)


# ─────────────────────────────────────────────────────────────────
# POST /api/episodes/review — Guardar nota de episódio
# Body: { episodioId, animeId, nota, userName }
# ─────────────────────────────────────────────────────────────────

@app.route('/api/episodes/review', methods=['POST'])
def review_episode():
    data = request.json
    if not data:
        return jsonify({'error': 'Body JSON inválido'}), 400

    episodio_id = str(data.get('episodioId', '')).strip()
    anime_id    = str(data.get('animeId', '')).strip()
    nota        = data.get('nota')
    user_name   = str(data.get('userName', 'Anônimo')).strip() or 'Anônimo'

    if not episodio_id:
        return jsonify({'error': 'episodioId é obrigatório'}), 400
    if not isinstance(nota, (int, float)) or nota < 1 or nota > 10:
        return jsonify({'error': 'nota deve ser entre 1 e 10'}), 400

    all_ratings = load_json(EPISODES_FILE, default={})

    if episodio_id not in all_ratings:
        all_ratings[episodio_id] = {}

    all_ratings[episodio_id][user_name] = int(nota)
    save_json(EPISODES_FILE, all_ratings)

    notas_vals  = [v for v in all_ratings[episodio_id].values() if isinstance(v, (int, float))]
    nota_media  = (sum(notas_vals) / len(notas_vals)) if notas_vals else 0.0
    total_notas = len(notas_vals)

    return jsonify({
        'id':          episodio_id,
        'animeId':     anime_id,
        'notaUsuario': int(nota),
        'notaMedia':   round(nota_media, 2),
        'totalNotas':  total_notas,
    }), 200


# ─────────────────────────────────────────────────────────────────
# Resenhas de anime
# ─────────────────────────────────────────────────────────────────

@app.route('/api/reviews', methods=['GET'])
def get_reviews():
    reviews = load_json(DATA_FILE)
    reviews.sort(key=lambda x: x.get('timestamp', ''), reverse=True)
    return jsonify(reviews)


@app.route('/api/reviews', methods=['POST'])
def add_review():
    data = request.json
    if not data:
        return jsonify({'error': 'Body inválido'}), 400

    review = {
        'id':          datetime.now().isoformat(),
        'anime_id':    data.get('anime_id') or data.get('animeId'),
        'anime_title': data.get('anime_title') or data.get('animeTitle'),
        'anime_image': data.get('anime_image') or data.get('animeImage'),
        'username':    data.get('username', 'Anônimo'),
        'rating':      data.get('rating'),
        'review':      data.get('review'),
        'has_spoiler': data.get('has_spoiler') or data.get('hasSpoiler', False),
        'likes':       0,
        'timestamp':   datetime.now().isoformat(),
    }
    reviews = load_json(DATA_FILE)
    reviews.append(review)
    save_json(DATA_FILE, reviews)
    return jsonify(review), 201


@app.route('/api/reviews/<review_id>/like', methods=['POST'])
def like_review(review_id):
    reviews = load_json(DATA_FILE)
    target  = None
    for r in reviews:
        if r['id'] == review_id:
            r['likes'] = r.get('likes', 0) + 1
            target = r
            break
    if target:
        save_json(DATA_FILE, reviews)
        return jsonify({'likes': target['likes']})
    return jsonify({'error': 'Resenha não encontrada'}), 404


# ─────────────────────────────────────────────────────────────────
# Health check
# ─────────────────────────────────────────────────────────────────

@app.route('/api/health', methods=['GET'])
def health():
    return jsonify({
        'status':       'ok',
        'cache_entries': len(_cache),
        'timestamp':    datetime.now().isoformat(),
    })


if __name__ == '__main__':
    app.run(debug=True, port=5000)
