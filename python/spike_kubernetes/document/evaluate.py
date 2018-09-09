from flask import Flask
import spike_kubernetes.document.helpers as document_helpers

app = Flask(__name__)


@app.route("/", methods=["POST"])
def index():
    return document_helpers.index_()
