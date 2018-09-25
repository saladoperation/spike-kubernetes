import allennlp.modules.conditional_random_field as conditional_random_field
import torch
import torch.nn as nn
import torch.nn.init as init
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.helpers as helpers
import spike_kubernetes.aid as aid

pretrained = vocab.Vectors("deps.words")
document_name = "document"
selection = helpers.get_selection(document_name)
tuned = helpers.get_tuned(document_name)


def get_embedding_vectors(unk_vector):
    return torch.cat((pretrained.vectors, unk_vector))


get_embedding = comp(partial(aid.flip(nn.Embedding.from_pretrained), False),
                     get_embedding_vectors)
multiply = comp(partial(reduce, operator.mul, 1),
                vector)
get_bidirectional_size = partial(multiply, 2)
num_tags = multiply(3, 2)


def get_model():
    # TODO possibly use embedding dropout
    model = nn.ModuleDict(
        {"embedding": get_embedding(
            init.kaiming_normal_(torch.zeros(1,
                                             pretrained.dim))),
            # TODO possibly use variational dropout
            # TODO possibly use layer normalization
            "lstm": nn.LSTM(pretrained.dim,
                            tuned["hidden_size"],
                            tuned["num_layers"],
                            batch_first=True,
                            bidirectional=True,
                            dropout=tuned["dropout"]),
            "linear": nn.Linear(
                get_bidirectional_size(tuned["hidden_size"]),
                num_tags),
            "crf": conditional_random_field.ConditionalRandomField(num_tags)})
    return model


def get_states(batch_size):
    return tuple(
        # TODO possibly don't use kaiming_normal_
        map(init.kaiming_normal_,
            repeat(2,
                   torch.zeros(get_bidirectional_size(tuned["num_layers"]),
                               batch_size,
                               tuned["hidden_size"]))))


index_ = helpers.make_index_(
    {helpers.get_stoi_name: constantly(pretrained.stoi)})
