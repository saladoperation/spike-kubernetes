import os
import os.path as path
import threading
import pika
import requests
import torch
from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid
from spike_kubernetes.cheshire import *
import spike_kubernetes.specter as s
import spike_kubernetes.helpers as helpers
import spike_kubernetes.document.evaluation as evaluation

channel = pika.BlockingConnection().channel()
queue = "queue"
channel.queue_declare(queue)
get_steps = comp(partial(map, comp(evaluation.convert_list,
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


def mkdirs(path_):
    os.makedirs(path_, exist_ok=True)


def make_plus(f):
    return comp(juxt(comp(mkdirs,
                          path.dirname,
                          path.abspath,
                          last),
                     partial(apply, f)),
                vector)


save_plus = make_plus(torch.save)


def log_pt(m):
    save_plus(s.setval_("training",
                        s.transform_(s.multi_path("model", "optimizer"),
                                     aid.make_attribute_call("state_dict"),
                                     m),
                        evaluation.progress),
              helpers.get_pt_path(evaluation.document_name))


def run_step(reduction, step):
    reduction["model"].train()
    reduction["model"].zero_grad()
    forwarded = evaluation.forward(
        merge(s.transform_(("states",
                            s.ALL),
                           aid.make_attribute_call("detach"),
                           reduction),
              step))
    forwarded["loss"].backward()
    reduction["optimizer"].step()
    reduction["model"].eval()
    return aid.if_then(
        comp(zero_,
             aid.build(mod,
                       partial(aid.flip(get), "global_step"),
                       partial(aid.flip(get),
                               "validation-interval"))),
        evaluation.convert_merge(comp(assess_remotely,
                                      partial(aid.flip(dissoc),
                                                    "states"),
                                      helpers.effect(log_pt),
                                      evaluation.set_inference)),
        merge(reduction, step, forwarded))


run_steps = partial(reduce,
                    run_step,
                    evaluation.progress["training"],
                    steps)
threading.Thread(target=run_steps).start()
