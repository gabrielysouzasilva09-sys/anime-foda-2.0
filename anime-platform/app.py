from flask import Flask, render_template, request, jsonify
import json
import os
from datetime import datetime

app = Flask(__name__)

DATA_FILE = 'reviews.json'

def load_reviews():
    if os.path.exists(DATA_FILE):
        with open(DATA_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    return []

def save_reviews(reviews):
    with open(DATA_FILE, 'w', encoding='utf-8') as f:
        json.dump(reviews, f, ensure_ascii=False, indent=2)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/reviews', methods=['GET'])
def get_reviews():
    reviews = load_reviews()
    # Ordenar por mais recentes
    reviews.sort(key=lambda x: x.get('timestamp', ''), reverse=True)
    return jsonify(reviews)

@app.route('/api/reviews', methods=['POST'])
def add_review():
    data = request.json
    review = {
        'id': datetime.now().isoformat(),
        'anime_id': data.get('anime_id'),
        'anime_title': data.get('anime_title'),
        'anime_image': data.get('anime_image'),
        'username': data.get('username', 'Anônimo'),
        'rating': data.get('rating'),
        'review': data.get('review'),
        'has_spoiler': data.get('has_spoiler', False),
        'likes': 0,
        'timestamp': datetime.now().isoformat()
    }
    reviews = load_reviews()
    reviews.append(review)
    save_reviews(reviews)
    return jsonify(review), 201

@app.route('/api/reviews/<review_id>/like', methods=['POST'])
def like_review(review_id):
    reviews = load_reviews()
    for r in reviews:
        if r['id'] == review_id:
            r['likes'] = r.get('likes', 0) + 1
            break
    save_reviews(reviews)
    return jsonify({'likes': r.get('likes', 0)})

if __name__ == '__main__':
    app.run(debug=True, port=5000)