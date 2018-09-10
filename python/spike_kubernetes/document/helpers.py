import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.helpers as helpers

pretrained = vocab.Vectors("deps.words")
get_stoi = constantly(pretrained.stoi)
index_ = helpers.make_index_({"get-stoi": get_stoi})
