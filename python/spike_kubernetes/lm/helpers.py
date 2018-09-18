import funcy
import itertools
import torch
from torch.nn import init
import torch.nn as nn
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.clojure.java.io as io
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


# TODO possibly parameterize freeze
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
get_lm_path = partial(io.file, "../resources/lm")
selection_path = get_lm_path("selection.json")
selection = parse_string(slurp(selection_path))
get_run_path = partial(get_lm_path, "runs", selection["run"])
tuned_path = get_run_path("tuned.json")
tuned_json = slurp(tuned_path)
tuned = parse_string(tuned_json)


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


index_ = helpers.make_index_({"get-stoi": constantly(stoi)})
