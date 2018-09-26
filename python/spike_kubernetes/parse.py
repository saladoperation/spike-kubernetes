from flask import Flask, jsonify, request
import spacy
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.set as set_
import spike_kubernetes.aid as aid
import spike_kubernetes.specter as s

app = Flask(__name__)


def get_token(token):
    return set_.rename_keys(s.transform_("head",
                                         partial(aid.flip(getattr), "i"),
                                         apply(merge,
                                               map(aid.build(array_map,
                                                             identity,
                                                             partial(getattr,
                                                                     token)),
                                                   {"dep_",
                                                    "head",
                                                    "i",
                                                    "is_sent_start",
                                                    "is_title",
                                                    "lemma_",
                                                    "lower_",
                                                    "tag_",
                                                    "text_with_ws",
                                                    "whitespace_"}))),
                            {"head": "head_i"})


nlp = spacy.load("en")
parse = comp(tuple,
             partial(map, get_token),
             nlp)


@app.route("/", methods=["POST"])
def index():
    return jsonify(parse(request.get_json()))
