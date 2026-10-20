/* ════════════════════════════════════════════════════════════════
   AniReview — Integração com o back-end (Spring Boot + PostgreSQL + JWT)
   ════════════════════════════════════════════════════════════════
   Funções:
     - login(username, password)      → POST /api/auth/login, guarda o token
     - logout()                       → remove o token do localStorage
     - submitReviewApi({...})         → POST /api/reviews com Authorization: Bearer <token>
     - authHeaders()                  → cabeçalhos com o token para pedidos autenticados
   ════════════════════════════════════════════════════════════════ */

var API_BASE  = 'http://localhost:8080';
var TOKEN_KEY = 'anireview_token';
var USER_KEY  = 'anireview_user';

/* ── Gestão do token JWT no localStorage ───────────────────────── */

function saveToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

function isLoggedIn() {
    return !!getToken();
}

function saveUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function getCurrentUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY));
    } catch (e) {
        return null;
    }
}

/**
 * Cabeçalhos para pedidos autenticados, incluindo o token JWT.
 * O back-end lê o cabeçalho "Authorization: Bearer <token>" e deriva
 * o autor a partir dele (não é possível falsificar identidade).
 */
function authHeaders(extra) {
    var headers = { 'Content-Type': 'application/json' };
    if (extra) {
        for (var k in extra) {
            if (Object.prototype.hasOwnProperty.call(extra, k)) headers[k] = extra[k];
        }
    }
    var token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    return headers;
}

/* ── LOGIN ─────────────────────────────────────────────────────── */

/**
 * Autentica o utilizador no back-end e guarda o token JWT no localStorage.
 *
 * @param {string} username
 * @param {string} password
 * @returns {Promise<Object>} dados do utilizador ({ id, username, email })
 */
function login(username, password) {
    return fetch(API_BASE + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(function (response) {
        if (!response.ok) {
            return response.json().catch(function () { return {}; }).then(function (erro) {
                throw new Error(erro.error || 'Credenciais inválidas');
            });
        }
        return response.json();
    })
    .then(function (data) {
        saveToken(data.token);
        saveUser({ id: data.id, username: data.username, email: data.email });

        if (typeof showToast === 'function') {
            showToast('✅ Sessão iniciada: ' + data.username);
        }
        return data;
    });
}

/* ── LOGOUT ────────────────────────────────────────────────────── */

function logout() {
    clearToken();
    if (typeof showToast === 'function') {
        showToast('👋 Sessão terminada');
    }
}

/* ── ENVIAR RESENHA (autenticado) ──────────────────────────────── */

/**
 * Envia uma resenha para o back-end. Requer sessão iniciada.
 *
 * @param {Object} review
 * @param {string|number} review.animeId
 * @param {string} review.animeTitle
 * @param {string} review.animeImage
 * @param {number} review.rating           nota de 1 a 10
 * @param {string} review.comment          texto da resenha
 * @param {boolean} [review.containsSpoiler]
 * @returns {Promise<Object>} resenha criada (com id, userName e createdAt do servidor)
 */
function submitReviewApi(review) {
    if (!getToken()) {
        return Promise.reject(new Error('Precisa de iniciar sessão para publicar uma resenha'));
    }

    var payload = {
        animeId:         String(review.animeId),
        animeTitle:      review.animeTitle,
        animeImage:      review.animeImage,
        rating:          parseInt(review.rating, 10),
        comment:         review.comment,
        containsSpoiler: !!review.containsSpoiler
    };

    return fetch(API_BASE + '/api/reviews', {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(payload)
    })
    .then(function (response) {
        if (response.status === 401 || response.status === 403) {
            clearToken();
            throw new Error('Sessão expirada — inicie sessão novamente');
        }
        if (!response.ok) {
            return response.json().catch(function () { return {}; }).then(function (erro) {
                throw new Error(erro.error || 'Erro ao enviar resenha');
            });
        }
        return response.json();
    })
    .then(function (saved) {
        if (typeof showToast === 'function') {
            showToast('✅ Resenha enviada com sucesso!');
        }
        return saved;
    });
}

/* ── CARREGAR RESENHAS DO SERVIDOR ─────────────────────────────── */

/**
 * Vai buscar as resenhas guardadas no servidor e mostra-as na página
 * (secção "Resenhas da Comunidade" → #reviewsFeed).
 *
 * @param {string|number} [animeId] se indicado, devolve só as resenhas desse anime
 * @returns {Promise<Array>} lista de resenhas devolvida pelo servidor
 */
function loadReviews(animeId) {
    var url = API_BASE + '/api/reviews';
    if (animeId !== undefined && animeId !== null && animeId !== '') {
        url += '?animeId=' + encodeURIComponent(animeId);
    }

    return fetch(url, { method: 'GET', headers: { 'Accept': 'application/json' } })
        .then(function (response) {
            if (!response.ok) {
                return response.json().catch(function () { return {}; }).then(function (erro) {
                    throw new Error(erro.error || ('Erro HTTP ' + response.status));
                });
            }
            return response.json();
        })
        .then(function (reviews) {
            if (typeof renderReviewsFeed === 'function') {
                renderReviewsFeed(reviews);
            }
            return reviews;
        });
}

/**
 * Igual a loadReviews(animeId), mas para o anime que está aberto no modal.
 */
function loadReviewsForCurrentAnime() {
    var anime = (typeof currentAnime !== 'undefined' && currentAnime) ? currentAnime : null;
    if (!anime || anime.id === undefined) {
        return Promise.reject(new Error('Nenhum anime aberto'));
    }
    return loadReviews(anime.id);
}

/* Carrega as resenhas do servidor ao abrir a página.
   Se a API estiver offline, mantém o feed local (localStorage). */
document.addEventListener('DOMContentLoaded', function () {
    loadReviews().catch(function (e) {
        console.warn('[AniReview] Não foi possível carregar as resenhas do servidor: ' + e.message);
    });
});

