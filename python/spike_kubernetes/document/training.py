from flask import Flask
import pika
import spike_kubernetes.document.helpers as document_helpers

app = Flask(__name__)


@app.route("/", methods=["POST"])
def index():
    return document_helpers.index_()


channel = pika.BlockingConnection().channel()
queue = "queue"
channel.queue_declare(queue)
