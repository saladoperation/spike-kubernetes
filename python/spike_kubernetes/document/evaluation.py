import allennlp.modules.conditional_random_field as conditional_random_field
from flask import Flask
from tensorboardX import SummaryWriter
import torch
import torch.nn as nn
import torch.nn.init as init
import torchtext.vocab as vocab
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid
import spike_kubernetes.specter as s
import spike_kubernetes.helpers as helpers

app = Flask(__name__)
pretrained = vocab.Vectors("deps.words")
document_name = "document"
selection = helpers.get_selection(document_name)
tuned = helpers.get_tuned(document_name)


def get_embedding_vectors(unk_vector):
    return torch.cat((pretrained.vectors, unk_vector))


get_embedding = comp(nn.Embedding.from_pretrained,
                     get_embedding_vectors)
get_bidirectional_size = partial(multiply, 2)
num_tags = multiply(3, 2)


def get_model():
    # TODO possibly use embedding dropout
    return helpers.move(
        nn.ModuleDict(
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
                "crf": conditional_random_field.ConditionalRandomField(
                    num_tags)}))


def forward(m):
    lstm_output, states = m["model"]["lstm"](
        m["model"]["embedding"](m["source"]),
        m["states"])
    output = m["model"]["linear"](lstm_output)
    # TODO don't pass mask when ConditionalRandomField gets fixed
    # If mask is not passed, crf gives an error on a GPU.
    # RuntimeError: Expected object of type torch.cuda.FloatTensor but found type torch.FloatTensor for argument #2 'other'
    return {"loss": -m["model"]["crf"](output,
                                       m["reference"],
                                       torch.ones_like(m["reference"])),
            "output": output,
            "states": states}


def get_states(batch_size):
    # TODO possibly don't use kaiming_normal_
    return tuple(
        map(init.kaiming_normal_,
            repeat(
                2,
                helpers.move(
                    torch.zeros(get_bidirectional_size(tuned["num_layers"]),
                                batch_size,
                                tuned["hidden_size"])))))


deep_merge = partial(helpers.deep_merge_with, comp(last,
                                                   vector))
validation_batch_size = 1
progress = deep_merge(
    {"training": {"states": get_states(tuned["batch-size"])},
     "validation": {"batch-size": validation_batch_size,
                    "states": get_states(validation_batch_size)}},
    helpers.get_progress_(document_name, get_model()))
convert_list = partial(s.transform_,
                       s.multi_path("source", "reference"),
                       comp(helpers.move,
                            torch.tensor))


def decode(crf, logits):
    return map(first,
               crf.viterbi_tags(logits,
                                torch.ones(tuple(drop_last(logits.size())),
                                           dtype=torch.uint8)))


get_inference = aid.build(decode,
                          partial(s.select_, ("model", "crf")),
                          partial(aid.flip(get), "output"))


def transfer_(apath, f, m):
    return s.setval_(apath, f(m), m)


set_inference = partial(transfer_, "inference", get_inference)


def convert_merge_(f, m):
    return aid.build(merge,
                     identity,
                     f)(m)


convert_merge = aid.curry(convert_merge_)
evaluate = comp(helpers.get_serializable,
                partial(aid.flip(select_keys), {"loss", "inference"}),
                set_inference,
                convert_merge(forward),
                partial(merge, progress["training"], progress["validation"]),
                convert_list)


def log_tensorboard(coll):
    writer = SummaryWriter(helpers.get_run_path(document_name, "tensorboard"))
    run_(partial(apply, writer.add_scalars), coll)
    writer.close()


@app.route(helpers.root_path, methods=helpers.methods)
def index():
    return helpers.index_(
        {helpers.get_stoi_name: constantly(pretrained.stoi),
         helpers.evaluate_name: evaluate,
         "log-tensorboard": log_tensorboard})
