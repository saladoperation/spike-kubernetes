import os.path as path
from flask import jsonify, request
import torch
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid

torch.manual_seed(0)
get_stoi_name = "get-stoi"


def make_index_(m):
    def index_():
        return jsonify(apply(m[request.get_json()["action"]],
                             ((request.get_json()["data"]),
                              ) if contains_(request.get_json(),
                                             "data") else ()))
    return index_


str = comp(str_join,
           vector)
get_json_name = partial(aid.flip(str), ".json")
selection_name = "selection"
selection_filename = get_json_name(selection_name)
get_resources_path = partial(path.join, "../resources")
get_selection_path = partial(aid.flip(get_resources_path),
                             get_json_name(selection_name))
tuned_name = get_json_name("tuned")


def get_tuned_path(model_name, timestamp):
    return get_resources_path(model_name, "runs", timestamp, tuned_name)
