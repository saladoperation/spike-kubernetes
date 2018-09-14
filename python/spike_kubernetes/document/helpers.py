import torchtext.vocab as vocab
import spike_kubernetes.helpers as helpers

pretrained = vocab.Vectors("deps.words")
index_ = helpers.make_index_({"get-stoi": helpers.make_get_stoi(pretrained)})
