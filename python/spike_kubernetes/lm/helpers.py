import itertools
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
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
index_ = helpers.make_index_({"get-stoi": constantly(stoi)})
