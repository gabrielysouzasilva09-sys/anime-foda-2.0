# AniReview - Plataforma de Animes

Uma plataforma interativa para avaliar animes e ver resenhas da comunidade.

## Como rodar

1. Instalar dependências:
```bash
pip install -r requirements.txt
```

2. Rodar o servidor:
```bash
python app.py
```

3. Abrir no navegador:
```
http://localhost:5000
```

## Funcionalidades

- Busca de animes via Jikan API (MyAnimeList)
- Catálogo com cards e notas
- Modal de detalhes com sinopse
- Sistema de resenhas (nota 1-10, texto, spoiler)
- Feed de resenhas da comunidade
- Curtir resenhas

## Estrutura

```
anime-platform/
├── app.py           # Backend Flask
├── reviews.json     # Banco de dados
├── requirements.txt # Dependências
└── templates/
    └── index.html   # Frontend completo
```