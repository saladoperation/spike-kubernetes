import torchtext.vocab as vocab
import spike_kubernetes.helpers as helpers

pretrained = vocab.pretrained_aliases["glove.6B.300d"]()
index_ = helpers.make_index_({"get-stoi": helpers.make_get_stoi(pretrained)})
