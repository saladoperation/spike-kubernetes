from flask import jsonify, request
from spike_kubernetes.clojure.core import *


def make_index_(m):
    def index_():
        return jsonify(apply(m[request.get_json()["action"]],
                             ((request.get_json()["data"]),
                              ) if contains_(request.get_json(),
                                             "data") else ()))
    return index_
