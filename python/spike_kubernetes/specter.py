from spike_kubernetes.clojure.core import *


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
                              vals,
                              first),
                         walk_values)
