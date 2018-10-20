from flask import Flask
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.nn.init as init
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid
import spike_kubernetes.helpers as helpers
import spike_kubernetes.specter as s

app = Flask(__name__)
pretrained = vocab.pretrained_aliases["glove.6B.300d"]()
special_tokens = ("<unk>", "<sos>", "<eos>")
extension = apply(array_map, mapcat(vector,
                                    special_tokens,
                                    range()))
stoi = merge(s.transform_(s.MAP_VALS,
                          partial(plus, count(special_tokens)),
                          pretrained.stoi),
             extension)


def get_embedding_vectors(special_vectors):
    return torch.cat((special_vectors, pretrained.vectors))


get_embedding = comp(partial(aid.flip(nn.Embedding.from_pretrained), False),
                     get_embedding_vectors)
get_states = comp(nn.ParameterList,
                  partial(map, nn.Parameter))
get_character_vector = comp(helpers.move,
                            partial(get, vocab.CharNGram()))
character_vector_size = count(first(get_character_vector("")))
lm_name = "lm"
selection = helpers.get_selection(lm_name)
tuned = helpers.get_tuned(lm_name)


def get_direction_model():
    return nn.ModuleDict(
        {"states": get_states(
            init.kaiming_normal_(torch.zeros(2,
                                             tuned["num_layers"],
                                             tuned["hidden_size"]))),
            "lstm": nn.LSTM(plus(pretrained.dim, character_vector_size),
                            tuned["hidden_size"],
                            tuned["num_layers"],
                            batch_first=True,
                            dropout=tuned["dropout"]),
            "linear": nn.Linear(tuned["hidden_size"], pretrained.dim)})


def get_model():
    return helpers.get_model_(
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
                            helpers.move(
                                torch.zeros(
                                    count(tuned["cutoffs"]),
                                    pretrained.dim))))})}))


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
        return helpers.move(
            torch.zeros(
                m["product"])) if empty_(
            cluster["reference"]) else helpers.move(
            torch.zeros(
                m["product"])).index_copy_(
            0,
            cluster["index"],
            F.cross_entropy(
                F.linear(
                    m["model"][m["direction"]]["linear"](
                        m["hidden"])[cluster["mask"]],
                    (make_cat_tail(m["model"]["parameter"]["tail"]) if
                     zero_(cluster["minimum"]) else
                     identity)(
                        m["model"]["embedding"].weight.narrow(
                            0,
                            cluster["minimum"],
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
                 aid.make_attribute_call("byte")),
         partial(s.transform_,
                 s.multi_path("source",
                              ("clusters",
                               s.ALL,
                               s.multi_path("index",
                                            "mask",
                                            "reference"))),
                 comp(helpers.move,
                      torch.tensor))))
progress = helpers.get_progress_(lm_name, get_model())["training"]
evaluate = comp(tuple,
                partial(map,
                        comp(helpers.get_serializable,
                             forward,
                             partial(merge, progress),
                             convert_list)))


@app.route(helpers.root_path, methods=helpers.methods)
def index():
    return helpers.index_(
        {helpers.get_stoi_name: constantly(stoi),
         helpers.evaluate_name: evaluate})
