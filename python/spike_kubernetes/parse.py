from flask import Flask, jsonify, request
import spacy
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.walk as walk
import spike_kubernetes.aid as aid
import spike_kubernetes.specter as s
import spike_kubernetes.helpers as helpers

app = Flask(__name__)


def get_map_(ks, x):
    return apply(merge,
                 map(aid.build(array_map,
                               identity,
                               partial(getattr,
                                       x)),
                     ks))


get_map = aid.curry(get_map_)


def transform_map(reduction, element):
    return s.transform_(first(element), get_map(last(element)), reduction)


def reduce_map(coll, m):
    return reduce(transform_map, m, coll)


get_token = comp(partial(walk.prewalk, helpers.convert_map),
                 partial(reduce_map, (("head",
                                       {"i", "children", "tag_"}),
                                      (("head", "children", s.ALL),
                                       {"dep_"}))),
                 partial(get_map, {"dep_",
                                   "ent_type_",
                                   "head",
                                   "i",
                                   "is_lower",
                                   "is_sent_start",
                                   "is_title",
                                   "lemma_",
                                   "lower_",
                                   "tag_",
                                   "text_with_ws",
                                   "whitespace_"}))
nlp = spacy.load("en")
parse = comp(tuple,
             partial(map, get_token),
             nlp)


@app.route(helpers.root_path, methods=helpers.methods)
def index():
    return jsonify(parse(request.get_json()))
