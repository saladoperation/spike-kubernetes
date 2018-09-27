from flask import Flask
import spike_kubernetes.helpers as helpers
import spike_kubernetes.lm.helpers as document_helpers

app = Flask(__name__)


@app.route(helpers.root_path, methods=helpers.methods)
def index():
    return document_helpers.index_()
