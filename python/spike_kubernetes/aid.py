import inspect
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


def funcall(f, *more):
    return f() if empty_(more) else apply(f, more)


def get_currying_arity(f):
    return max(2, count(inspect.signature(f).parameters))


minus = comp(partial(reduce, operator.sub, 0),
             vector)


def curry(x, *outer_more):
    def call_middle(*middle_more):
        def call_inner(*inner_more):
            return first(outer_more)(*middle_more, *inner_more)
        return first(
            outer_more)(
            *middle_more) if equal(count(middle_more),
                                   x) else curry(minus(x,
                                                       count(middle_more)),
                                                 call_inner)
    return curry(get_currying_arity(x),
                 x) if zero_(count(outer_more)) else call_middle
