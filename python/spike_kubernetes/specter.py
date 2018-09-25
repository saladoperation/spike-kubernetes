from spike_kubernetes.clojure.core import *
import spike_kubernetes.aid as aid


class RichNavigator:
    def __init__(self, select__, transform__):
        self.select_ = select__
        self.transform_ = transform__


def update(m, k, f):
    return update_in(m, [k], f)


def get_transform_continuation(continuation, element):
    def continuation_(structure):
        return update(
            structure,
            element,
            continuation) if isinstance(
            element,
            builtins.str) else element.transform_(continuation,
                                                  structure)
    return continuation_


def coerce_path(path):
    return path if isinstance(path, tuple) else vector(path)


def transform_(path, transform_fn, structure):
    return reduce(get_transform_continuation,
                  transform_fn,
                  reverse(coerce_path(path)))(structure)


def vals(m):
    return m.values()


MAP_VALS = RichNavigator(comp(tuple,
                              vals),
                         walk_values)


def setval_(path, val, structure):
    return transform_(path, constantly(val), structure)


ALL = RichNavigator(vector, walk)


def multi_path(*paths):
    def transform_continuation_(continuation, structure):
        def transform__(structure_, path):
            return transform_(path, continuation, structure_)
        return reduce(transform__, structure, reverse(paths))
    return RichNavigator(partial(comp(tuple,
                                      mapcat),
                                 juxt(*map(aid.curry(2,
                                                     aid.flip(get)),
                                           paths))),
                         transform_continuation_)


def get_select_continuation(continuation, element):
    def continuation_(structure):
        return continuation(get(
            structure,
            element)) if isinstance(
            element,
            builtins.str) else element.select_(structure)
    return continuation_


def select_(path, structure):
    return reduce(get_select_continuation,
                  identity,
                  reverse(coerce_path(path)))(structure)
