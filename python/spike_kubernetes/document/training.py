from flask import Flask
import pika
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid
from spike_kubernetes.cheshire import *
import spike_kubernetes.specter as s
import spike_kubernetes.helpers as helpers
import spike_kubernetes.document.helpers as document_helpers

app = Flask(__name__)


@app.route("/", methods=["POST"])
def index():
    return document_helpers.index_()


channel = pika.BlockingConnection().channel()
queue = "queue"
channel.queue_declare(queue)


def make_attribute_call(s_):
    return comp(aid.build(partial(apply, aid.funcall),
                          comp(partial(aid.flip(getattr), s_),
                               first),
                          rest),
                vector)


get_steps = comp(partial(map, comp(document_helpers.convert_list,
                                   parse_string,
                                   make_attribute_call("decode"))),
                 partial(remove, partial(equal, None)),
                 partial(map, last))
steps = get_steps(repeatedly(partial(channel.basic_get, queue, True)))
mod = comp(second,
           divmod)


def run_step(reduction, step):
    reduction["model"].train()
    reduction["model"].zero_grad()
    forwarded = document_helpers.forward(
        merge(s.transform_(("states",
                            s.ALL),
                           make_attribute_call("detach"),
                           reduction),
              step))
    forwarded["loss"].backward()
    reduction["optimizer"].step()
    reduction["model"].eval()
    # TODO: implement this function
    return (comp(helpers.get_serializable,
                 partial(aid.flip(select_keys),
                         {"global_step",
                          "inference",
                          "loss",
                          "mask",
                          "minimum"}),
                 document_helpers.set_inference) if
            zero_(mod(step["global_step"],
                      document_helpers.tuned["validation-interval"])) else
            identity)(merge(reduction,
                            step,
                            forwarded))
