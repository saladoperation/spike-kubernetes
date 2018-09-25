import os.path as path
from flask import jsonify, request
import torch
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.string as str_
import spike_kubernetes.aid as aid
from spike_kubernetes.cheshire import *

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
append_extension = comp(partial(str_.join, "."),
                        vector)
json_name = "json"
get_json_filename = partial(aid.flip(append_extension), json_name)
selection_name = "selection"
selection_filename = get_json_filename(selection_name)
get_resources_path = partial(path.join, "../resources")
get_selection_path = partial(aid.flip(get_resources_path),
                             get_json_filename(selection_name))
tuned_name = get_json_filename("tuned")


def get_run_path(model_name, run, filename):
    return get_resources_path(model_name, "runs", run, filename)


def get_selected_pt_path(model_name, run, step_selection):
    return get_resources_path(model_name,
                              run,
                              append_extension(step_selection, "pt"))


def get_selected_json_path(model_name, run, step_selection):
    return get_run_path(model_name,
                        run,
                        append_extension(step_selection, json_name))


def get_tuned_path(model_name, run):
    return get_run_path(model_name, run, tuned_name)


device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")


def get_checkpoint_path(selected_pth_path, selected_json_path):
    return merge(
        torch.load(selected_pth_path, map_location=device),
        parse_string(
            slurp(
                selected_json_path))["training"]) if path.exists(
        selected_pth_path) else {}
