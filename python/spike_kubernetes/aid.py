from spike_kubernetes.clojure.core import *


def build(f, *fs):
    return comp(partial(apply, f),
                apply(juxt, fs))


def flip(f):
    def g(x, *more):
        def h(y, *more_):
            return apply(f, y, x, more_)
        return h if empty_(more) else apply(f, first(more), x, rest(more))
    return g
