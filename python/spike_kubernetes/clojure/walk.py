from spike_kubernetes.clojure.core import *


def walk(inner, outer, form):
    return outer(funcy.walk(inner, form) if
                 contains_({dict, list, tuple}, type(form)) else
                 form)


def prewalk(f, form):
    return walk(partial(prewalk, f), identity, f(form))
