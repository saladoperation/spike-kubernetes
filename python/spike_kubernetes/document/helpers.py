import torch
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.helpers as helpers

pretrained = vocab.Vectors("deps.words")
index_ = helpers.make_index_({"get-stoi": constantly(pretrained.stoi)})


def get_embedding_vectors(unk_vector):
    return torch.cat((pretrained.vectors, unk_vector))
