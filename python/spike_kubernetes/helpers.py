from flask import jsonify, request
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid

make_get_stoi = comp(constantly,
                     partial(aid.flip(getattr), "stoi"))


def make_index_(m):
    def index_():
        return jsonify(apply(m[request.get_json()["action"]],
                             ((request.get_json()["data"]),
                              ) if contains_(request.get_json(),
                                             "data") else ()))
    return index_
