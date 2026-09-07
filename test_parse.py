import json
import sys

def parse_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    try:
        data = json.loads(content)
        print(f"Workers: {len(data.get('workers', []))}")
        print("Success")
    except Exception as e:
        print(f"Error: {e}")

parse_file('user_json.txt')
