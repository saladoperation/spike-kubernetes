from flask import Flask
import pika
import requests
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
get_steps = comp(partial(map, comp(document_helpers.convert_list,
                                   parse_string,
                                   aid.make_attribute_call("decode"))),
                 partial(remove, partial(equal, None)),
                 partial(map, last))
steps = get_steps(repeatedly(partial(channel.basic_get, queue, True)))


def post_json(url, json_):
    return requests.post(url, json=json_)


assess_remotely = comp(aid.make_attribute_call("json"),
                       partial(post_json, "http://localhost:8080"),
                       helpers.get_serializable)


def run_step(reduction, step):
    reduction["model"].train()
    reduction["model"].zero_grad()
    forwarded = document_helpers.forward(
        merge(s.transform_(("states",
                            s.ALL),
                           aid.make_attribute_call("detach"),
                           reduction),
              step))
    forwarded["loss"].backward()
    reduction["optimizer"].step()
    reduction["model"].eval()
    # TODO: implement this function
    return aid.if_then(comp(zero_,
                            aid.build(mod,
                                      partial(aid.flip(get),
                                              "global_step"),
                                      partial(aid.flip(get),
                                              "validation-interval"))),
                       comp(assess_remotely,
                            document_helpers.set_inference),
                       merge(reduction, step, forwarded))
