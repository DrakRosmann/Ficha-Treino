#!/usr/bin/env python3
"""Gera os JSON de dados do app Android a partir dos arquivos .js do PWA.

Uso (na raiz do repositório):  python3 android/tools/convert_data.py

Lê exercises.js, templates.js e foods.js e grava em android/app/src/main/assets/data/.
Rode de novo sempre que o catálogo, os modelos ou os alimentos mudarem no PWA.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'android', 'app', 'src', 'main', 'assets', 'data')


class JsLiteral:
    """Lê um valor literal de JavaScript (array, objeto, string, número, true/false/null)."""

    def __init__(self, src, pos=0):
        self.s, self.i = src, pos

    def ws(self):
        s = self.s
        while self.i < len(s):
            c = s[self.i]
            if c in ' \t\r\n':
                self.i += 1
            elif s.startswith('//', self.i):
                self.i = s.index('\n', self.i) if '\n' in s[self.i:] else len(s)
            elif s.startswith('/*', self.i):
                self.i = s.index('*/', self.i) + 2
            else:
                break

    def value(self):
        self.ws()
        c = self.s[self.i]
        if c == '[':
            return self.array()
        if c == '{':
            return self.obj()
        if c in '\'"`':
            return self.string()
        m = re.compile(r'-?(\d+\.?\d*|\.\d+)([eE][-+]?\d+)?').match(self.s, self.i)
        if m:
            self.i = m.end()
            t = m.group(0)
            return float(t) if any(ch in t for ch in '.eE') else int(t)
        m = re.compile(r'[A-Za-z_$][\w$]*').match(self.s, self.i)
        if m:
            self.i = m.end()
            word = m.group(0)
            if word in ('true', 'false'):
                return word == 'true'
            if word in ('null', 'undefined'):
                return None
            raise ValueError(f'identificador inesperado {word!r} em {self.i}')
        raise ValueError(f'caractere inesperado {c!r} em {self.i}')

    def string(self):
        q = self.s[self.i]
        self.i += 1
        out = []
        while True:
            c = self.s[self.i]
            if c == '\\':
                n = self.s[self.i + 1]
                if n == 'u':
                    out.append(chr(int(self.s[self.i + 2:self.i + 6], 16)))
                    self.i += 6
                    continue
                out.append({'n': '\n', 't': '\t', 'r': '\r', '0': '\0'}.get(n, n))
                self.i += 2
                continue
            if c == q:
                self.i += 1
                return ''.join(out)
            out.append(c)
            self.i += 1

    def array(self):
        self.i += 1
        out = []
        while True:
            self.ws()
            if self.s[self.i] == ']':
                self.i += 1
                return out
            out.append(self.value())
            self.ws()
            if self.s[self.i] == ',':
                self.i += 1

    def obj(self):
        self.i += 1
        out = {}
        while True:
            self.ws()
            if self.s[self.i] == '}':
                self.i += 1
                return out
            if self.s[self.i] in '\'"':
                key = self.string()
            else:
                m = re.compile(r'[\w$]+').match(self.s, self.i)
                key = m.group(0)
                self.i = m.end()
            self.ws()
            assert self.s[self.i] == ':', f'esperava ":" em {self.i}'
            self.i += 1
            out[key] = self.value()
            self.ws()
            if self.s[self.i] == ',':
                self.i += 1


def const(src, name):
    m = re.search(r'\bconst\s+' + re.escape(name) + r'\s*=\s*', src)
    if not m:
        raise KeyError(name)
    return JsLiteral(src, m.end()).value()


def read(name):
    with open(os.path.join(ROOT, name), encoding='utf-8') as f:
        return f.read()


def write(name, data):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, separators=(',', ':'))
    print(f'{os.path.relpath(path, ROOT)}: {os.path.getsize(path) // 1024} KB')


def exercises():
    src = read('exercises.js')
    rows = const(src, 'BUILTIN_EXERCISES')
    out = []
    for row in rows:
        ex_id, name, group, equip, kind, img, mus = (row + [None] * 7)[:7]
        p, _, s = (mus or '').partition('|')
        out.append({
            'id': ex_id, 'name': name, 'group': group, 'equip': equip, 'kind': kind or 'w',
            'img': img or None, 'en': (img or '').replace('_', ' '),
            'primary': [x for x in p.split(',') if x], 'secondary': [x for x in s.split(',') if x],
        })
    write('exercises.json', {
        'groups': const(src, 'GROUPS'), 'equipment': const(src, 'EQUIPMENT'),
        'kinds': const(src, 'KINDS'), 'muscles': const(src, 'MUSCLES'), 'exercises': out,
    })


def templates():
    src = read('templates.js')
    tpls = const(src, 'TEMPLATES')
    for t in tpls:
        for r in t['routines']:
            r['items'] = [{'exId': it[0], 'sets': it[1], 'reps': str(it[2]), 'rest': it[3] if len(it) > 3 else 90,
                           'note': it[4] if len(it) > 4 else ''} for it in r['items']]
    write('templates.json', tpls)


def body():
    src = read('body.js')
    write('body.json', {
        'front': const(src, 'BODY_FRONT'), 'back': const(src, 'BODY_BACK'),
        'regions': const(src, 'MUSCLE_REGIONS'), 'groupMuscles': const(src, 'GROUP_MUSCLES'),
    })


def foods():
    src = read('foods.js')
    write('foods.json', {
        'groups': const(src, 'FOOD_GROUPS'), 'taco': const(src, 'TACO_FOODS'), 'extra': const(src, 'EXTRA_FOODS'),
    })


if __name__ == '__main__':
    for fn in (exercises, templates, body, foods):
        try:
            fn()
        except Exception as e:  # noqa: BLE001 — mostra qual arquivo falhou e segue
            print(f'{fn.__name__}: {e}', file=sys.stderr)
            sys.exit(1)
