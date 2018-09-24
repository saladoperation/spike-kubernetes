import funcy
import itertools
import os.path as path
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.nn.init as init
import torch.optim as optim
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.string as str_
import spike_kubernetes.aid as aid
from spike_kubernetes.cheshire import *
import spike_kubernetes.helpers as helpers
import spike_kubernetes.specter as s

pretrained = vocab.pretrained_aliases["glove.6B.300d"]()
range = itertools.count
special_tokens = ("<unk>", "<sos>", "<eos>")
mapcat = comp(partial(apply, concat),
              map)
extension = apply(array_map, mapcat(vector,
                                    special_tokens,
                                    range()))
plus = comp(partial(reduce, operator.add, 0),
            vector)
stoi = merge(s.transform_(s.MAP_VALS,
                          partial(plus, count(special_tokens)),
                          pretrained.stoi),
             extension)


def get_embedding_vectors(special_vectors):
    return torch.cat((special_vectors, pretrained.vectors))


get_embedding = comp(partial(aid.flip(nn.Embedding.from_pretrained), False),
                     get_embedding_vectors)


def make_attribute_call(s_):
    return comp(aid.build(partial(apply, aid.funcall),
                          comp(partial(aid.flip(getattr), s_),
                               first),
                          rest),
                vector)


get_states = comp(nn.ParameterList,
                  partial(map, nn.Parameter))
device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
move = partial(aid.flip(make_attribute_call("to")), device)
get_character_vector = comp(move,
                            partial(get, vocab.CharNGram()))
character_vector_size = count(first(get_character_vector("")))
lm_name = "lm"
selection_path = helpers.get_selection_path(lm_name)
selection = parse_string(slurp(selection_path))
tuned_path = helpers.get_tuned_path(lm_name, selection["run"])
tuned = parse_string(slurp(tuned_path))


def get_direction_model():
    return nn.ModuleDict({"states": get_states(
        init.kaiming_normal_(torch.zeros(2,
                                         tuned["num_layers"],
                                         tuned["hidden_size"]))),
        "lstm": nn.LSTM(plus(pretrained.dim, character_vector_size),
                        tuned["hidden_size"],
                        tuned["num_layers"],
                        batch_first=True,
                        dropout=tuned["dropout"]),
        "linear": nn.Linear(tuned["hidden_size"], pretrained.dim)})


effect = aid.curry(2, comp(last,
                           last,
                           juxt(aid.funcall,
                                vector)))
zipmap = comp(partial(apply, merge),
              partial(map, array_map))
repeatedly = comp(partial(apply, funcy.repeatedly),
                  reverse,
                  vector)


def get_model():
    return move(
        effect(
            make_attribute_call("eval"),
            nn.ModuleDict(
                merge(
                    zipmap(("forth", "back"),
                           repeatedly(2, get_direction_model)),
                    {"embedding": get_embedding(
                        init.kaiming_normal_(torch.zeros(count(special_tokens),
                                                         pretrained.dim))),
                        # TODO delete nn.ParameterDict when indexing nn.Parameter gets supported
                        # nn.ParameterDict works around TypeError: torch.FloatTensor is not a Module subclass
                        "parameter": nn.ParameterDict(
                            {"tail": nn.Parameter(
                                init.kaiming_normal_(
                                    move(
                                        torch.zeros(
                                            count(tuned["cutoffs"]),
                                            pretrained.dim))))})}))))


def _flip(f):
    def g(x, *more):
        def h(y, *more_):
            return apply(f, y, x, more_)
        return h if empty_(more) else apply(f, first(more), x, rest(more))
    return g


map_ = partial(_flip(isinstance), dict)
true_ = partial(equal, True)
every_ = comp(empty_,
              partial(remove, true_),
              map)


def assoc(m, k, v):
    return set_in(m, (k,), v)


key = first
val = second


def merge_with(f, *maps):
    def merge_entry(m, e):
        return assoc(m,
                     key(e),
                     f(m[key(e)],
                       val(e)) if contains_(m, key(e)) else val(e))
    return reduce(partial(reduce, merge_entry), maps)


def deep_merge_with(f, *more):
    return merge_with(partial(deep_merge_with, f),
                      *more) if every_(map_, more) else f(*more)


deep_merge = partial(deep_merge_with, comp(last,
                                           vector))
get_checkpoints_path = partial(get_run_path, "checkpoints")
append_extension = comp(partial(str_.join, "."),
                        vector)
append_pth = partial(aid.flip(append_extension), "pth")
recent_filename = "recent"
step_selection = recent_filename if selection[recent_filename] else "minimum"
get_pth_path = comp(get_checkpoints_path,
                    append_pth)
selected_pth_path = get_pth_path(step_selection)
selected_json_path = get_checkpoints_path(append_extension(step_selection,
                                                           "json"))
checkpoint = deep_merge(
    torch.load(selected_pth_path, map_location=device),
    parse_string(
        slurp(
            selected_json_path))) if path.exists(
    selected_pth_path) else {"training": {}}


def transpose_batch(x):
    return torch.transpose(x, 0, 1).contiguous()


def make_get_batch_state(batch_size):
    def get_batch_state(state):
        return transpose_batch(state.expand(batch_size, *state.size()))
    return get_batch_state


def get_batch_states(m):
    return tuple(map(make_get_batch_state(m["batch-size"]),
                     m["model"][m["direction"]]["states"]))


def flatten_batch(tensor):
    return tensor.contiguous().view((-1, last(tensor.size())))


def make_cat_tail(tail):
    def cat_tail(x):
        return torch.cat((x, tail), 0)
    return cat_tail


def make_adaptive_cross_entropy(m):
    def adaptive_cross_entropy(cluster):
        return move(
            torch.zeros(
                m["product"])) if empty_(
            cluster["reference"]) else move(
            torch.zeros(
                m["product"])).index_copy_(
            0,
            cluster["index"],
            F.cross_entropy(
                F.linear(
                    m["model"][m["direction"]]["linear"](
                        m["hidden"])[cluster["mask"]],
                    (make_cat_tail(
                        m["model"]["parameter"]["tail"]) if
                     zero_(
                         cluster["infimum"]) else identity)(
                        m["model"]["embedding"].weight.narrow(
                            0,
                            cluster["infimum"],
                            cluster["length"]))),
                cluster["reference"],
                reduction="none"))
    return adaptive_cross_entropy


map_sum = comp(sum,
               map)


def forward_(m):
    return map_sum(
        make_adaptive_cross_entropy(
            s.setval_(
                "hidden",
                flatten_batch(
                    first(
                        m["model"][m["direction"]]["lstm"](
                            torch.cat(
                                (m["model"]["embedding"](
                                    m[m["direction"]]["source"]),
                                 m[m["direction"]]["character"]),
                                -1),
                            get_batch_states(m)))),
                m)),
        m[m["direction"]]["clusters"])


def forward(m):
    return map_sum(comp(forward_,
                        partial(assoc, m, "direction")),
                   ("forth", "back"))


def dorun(coll):
    for _ in coll:
        pass


run_ = comp(dorun,
            map)


def make_set_lr_(lr):
    def set_lr__(param_group):
        param_group["lr"] = lr
    return set_lr__


def set_lr_(optimizer, lr):
    run_(make_set_lr_(lr), optimizer.param_groups)


set_lr = aid.curry(2, set_lr_)


def load_state(state, entity):
    entity.load_state_dict(state)


load_states = partial(merge_with, load_state, checkpoint["training"])
get_optimizer = comp(optim.Adam,
                     partial(filter,
                             partial(aid.flip(getattr), "requires_grad")),
                     aid.funcall,
                     partial(aid.flip(getattr), "parameters"))
progress = merge(checkpoint,
                 effect(partial(s.transform_,
                                "optimizer",
                                aid.flip(set_lr)(tuned["lr"])),
                        effect(load_states,
                               zipmap(("model",
                                       "optimizer"),
                                      juxt(identity,
                                           get_optimizer)(get_model())))))
convert_list = partial(
    s.transform_,
    s.multi_path("forth", "back"),
    comp(partial(s.transform_,
                 "character",
                 comp(torch.cat,
                      tuple,
                      partial(map,
                              comp(partial(aid.flip(torch.unsqueeze),
                                           0),
                                   torch.cat,
                                   tuple,
                                   partial(map,
                                           get_character_vector))))),
         partial(s.transform_,
                 ("clusters", s.ALL, "mask"),
                 make_attribute_call("byte")),
         partial(s.transform_,
                 s.multi_path("source",
                              ("clusters",
                               s.ALL,
                               s.multi_path("index",
                                            "mask",
                                            "reference"))),
                 comp(move,
                      torch.tensor))))


def convert_tensor_(x):
    return x.tolist() if isinstance(x, torch.Tensor) else x


evaluate = comp(tuple,
                partial(map,
                        comp(convert_tensor_,
                             forward,
                             partial(merge, progress),
                             convert_list)))
index_ = helpers.make_index_({helpers.get_stoi_name: constantly(stoi),
                              "evaluate": evaluate})
