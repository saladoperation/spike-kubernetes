import torch
import torch.nn as nn
import torch.nn.init as init
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
from spike_kubernetes.cheshire import *
import spike_kubernetes.helpers as helpers
import spike_kubernetes.aid as aid

pretrained = vocab.Vectors("deps.words")
document_name = "document"
selection_path = helpers.get_selection_path(document_name)
selection = parse_string(slurp(selection_path))
tuned_path = helpers.get_tuned_path(document_name, selection["run"])
tuned = parse_string(slurp(tuned_path))


def get_embedding_vectors(unk_vector):
    return torch.cat((pretrained.vectors, unk_vector))


get_embedding = comp(partial(aid.flip(nn.Embedding.from_pretrained), False),
                     get_embedding_vectors)


def get_model():
    model = nn.Module()
    # TODO possibly use embedding dropout
    model.embedding = get_embedding(
        init.kaiming_normal_(torch.zeros(1,
                                         pretrained.dim)))
    # TODO possibly use variational dropout
    # TODO possibly use layer normalization
    model.lstm = nn.LSTM(pretrained.dim,
                         tuned["hidden_size"],
                         tuned["num_layers"],
                         batch_first=True,
                         bidirectional=True,
                         dropout=tuned["dropout"])
    # TODO implement this function
    return model


index_ = helpers.make_index_(
    {helpers.get_stoi_name: constantly(pretrained.stoi)})
