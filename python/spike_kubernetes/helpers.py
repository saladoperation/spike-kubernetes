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
get_selection = comp(parse_string,
                     slurp,
                     get_selection_path)
tuned_name = get_json_filename("tuned")


def get_run_path(model_name, run, filename):
    return get_resources_path(model_name, "runs", run, filename)


def get_tuned_path(model_name, selection):
    return get_run_path(model_name, selection["run"], tuned_name)


get_tuned = comp(parse_string,
                 slurp,
                 aid.build(get_tuned_path,
                           identity,
                           get_selection))

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

recent_name = "recent"


def get_step_selection(selection):
    return recent_name if selection[recent_name] else "minimum"


def get_checkpoint(model_name):
    return merge(
        torch.load(get_resources_path(model_name,
                                      get_selection(model_name)["run"],
                                      append_extension(
                                          get_step_selection(
                                              get_selection(model_name)),
                                          "pt")),
                   device),
        parse_string(
            slurp(
                get_resources_path(
                    model_name,
                    get_selection(model_name)["run"],
                    append_extension(
                        get_step_selection(
                            get_selection(model_name)),
                        json_name))))["training"]) if path.exists(
        get_resources_path(model_name,
                           get_selection(model_name)["run"])) else {}
