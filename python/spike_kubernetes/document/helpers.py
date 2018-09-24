import torch
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
from spike_kubernetes.cheshire import *
import spike_kubernetes.helpers as helpers

pretrained = vocab.Vectors("deps.words")
document_name = "document"
selection_path = helpers.get_selection_path(document_name)
selection = parse_string(slurp(selection_path))
tuned_path = helpers.get_tuned_path(document_name, selection["run"])
tuned = parse_string(slurp(tuned_path))

def get_embedding_vectors(unk_vector):
    return torch.cat((pretrained.vectors, unk_vector))


index_ = helpers.make_index_(
    {helpers.get_stoi_name: constantly(pretrained.stoi)})
