from flask import Flask, jsonify, request
import spacy
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid
import spike_kubernetes.specter as s
import spike_kubernetes.helpers as helpers

app = Flask(__name__)


def get_token(token):
    return dissoc(
        helpers.transfer_(
            "sibling_deps",
            comp(tuple,
                 partial(map, partial(aid.flip(getattr), "dep_")),
                 partial(aid.flip(getattr), "children"),
                 partial(s.select_, "head")),
            helpers.transfer_(
                "head_tag_",
                comp(partial(aid.flip(getattr), "tag_"),
                     partial(s.select_, "head")),
                helpers.transfer_("head_i",
                                  comp(partial(aid.flip(getattr), "i"),
                                       partial(s.select_, "head")),
                                  apply(merge,
                                        map(aid.build(array_map,
                                                      identity,
                                                      partial(getattr,
                                                              token)),
                                            {"dep_",
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
                                             "whitespace_"}))))),
        "head")


nlp = spacy.load("en")
parse = comp(tuple,
             partial(map, get_token),
             nlp)


@app.route(helpers.root_path, methods=helpers.methods)
def index():
    return jsonify(parse(request.get_json()))
