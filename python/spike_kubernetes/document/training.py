from flask import Flask
import pika
from spike_kubernetes.clojure.core import *
import spike_kubernetes.document.helpers as document_helpers

app = Flask(__name__)


@app.route("/", methods=["POST"])
def index():
    return document_helpers.index_()


channel = pika.BlockingConnection().channel()
queue = "queue"
channel.queue_declare(queue)
# TODO implement this function
get_steps = comp(partial(map, last))
steps = get_steps(repeatedly(partial(channel.basic_get, queue, True)))
