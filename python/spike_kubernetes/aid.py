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
                 x) if empty_(outer_more) else call_middle


def if_then_else_(if_function, then_funciton, else_function, x):
    return then_funciton(x) if if_function(x) else else_function(x)


if_then_else = curry(4, if_then_else_)


def if_then_(if_function, then_function, else_):
    return if_then_else(if_function, then_function, identity, else_)


if_then = curry(3, if_then_)


def if_else_(if_function, else_function, then):
    return if_then_else(if_function, identity, else_function, then)


if_else = curry(3, if_else_)


def make_attribute_call(s_):
    return comp(build(partial(apply, funcall),
                      comp(partial(flip(getattr), s_),
                           first),
                      rest),
                vector)
