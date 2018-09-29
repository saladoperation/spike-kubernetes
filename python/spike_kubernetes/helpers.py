import os.path as path
from flask import jsonify, request
import torch
import torch.optim as optim
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.string as str_
import spike_kubernetes.clojure.walk as walk
import spike_kubernetes.aid as aid
from spike_kubernetes.cheshire import *
import spike_kubernetes.specter as s

torch.manual_seed(0)
get_stoi_name = "get-stoi"
evaluate_name = "evaluate"


def make_index_(m):
    def index_():
        return jsonify(apply(m[request.get_json()["action"]],
                             ((request.get_json()["data"]),) if
                             contains_(request.get_json(), "data")
                             else ()))
    return index_


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


get_optimizer = comp(optim.Adam,
                     partial(filter,
                             partial(aid.flip(getattr), "requires_grad")),
                     aid.funcall,
                     partial(aid.flip(getattr), "parameters"))


def make_set_lr_(lr):
    def set_lr__(param_group):
        param_group["lr"] = lr
    return set_lr__


def dorun(coll):
    for _ in coll:
        pass


run_ = comp(dorun,
            map)


def set_lr_(optimizer, lr):
    run_(make_set_lr_(lr), optimizer.param_groups)


set_lr = aid.curry(2, set_lr_)


def load_state(state, entity):
    entity.load_state_dict(state)


effect = aid.curry(2, comp(last,
                           last,
                           juxt(aid.funcall,
                                vector)))


def get_pt_path(model_name):
    return get_resources_path(model_name,
                              get_selection(model_name)["run"],
                              append_extension(
                                  get_step_selection(
                                      get_selection(model_name)),
                                  "pt"))


def effects(*more):
    run_(partial(aid.flip(aid.funcall), last(more)), drop_last(more))
    return last(more)


def get_training_progress(model_name, model):
    return merge(get_tuned(model_name),
                 effects(partial(s.transform_,
                                 "optimizer",
                                 aid.flip(set_lr)(get_tuned(model_name)["lr"])),
                         partial(merge_with,
                                 load_state,
                                 torch.load(get_pt_path(model_name), device) if
                                 path.exists(get_pt_path(model_name)) else
                                 {}),
                         zipmap(("model",
                                 "optimizer"),
                                juxt(identity,
                                     get_optimizer)(model))))


convert_map = aid.if_then(comp(partial(equal, builtins.map),
                               type),
                          tuple)
convert_tensor = aid.if_then(comp(partial(equal, torch.Tensor),
                                  type),
                             aid.make_attribute_call("tolist"))
filter_map = comp(partial(into, {}),
                  filter)
serializable_ = aid.build(or_,
                          number_,
                          string_,
                          is_seqcoll,
                          map_)
filter_map_serializable = aid.if_then(map_,
                                      partial(filter_map,
                                              comp(serializable_,
                                                   val)))
get_serializable = comp(filter_map_serializable,
                        partial(walk.prewalk, comp(convert_tensor,
                                                   convert_map)))
root_path = "/"
methods = ("POST",)
