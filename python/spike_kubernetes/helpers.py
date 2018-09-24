from flask import jsonify, request
import torch
from spike_kubernetes.clojure.core import *

torch.manual_seed(0)
get_stoi_name = "get-stoi"


def make_index_(m):
    def index_():
        return jsonify(apply(m[request.get_json()["action"]],
                             ((request.get_json()["data"]),
                              ) if contains_(request.get_json(),
                                             "data") else ()))
    return index_
