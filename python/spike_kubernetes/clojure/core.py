import builtins
import functools
import operator
from funcy import *

drop_last = butlast


def apply(f, *more):
    return f(*drop_last(more), *last(more))


def get_items(coll):
    return coll.items() if isinstance(coll, dict) else coll


comp = compose


def map(f, *colls):
    return builtins.map(f, *builtins.map(get_items, colls))


contains_ = operator.contains
count = comp(len,
             tuple)


def equal(*more):
    return operator.eq(
        first(more),
        last(more)) if operator.eq(count(more),
                                   2) else equal(first(more),
                                                 equal(*rest(more)))


def reduce(f, *more):
    return functools.reduce(f,
                            get_items(last(more)),
                            *() if equal(count(more), 1) else (first(more),))


def vector(*more):
    return tuple(more)


reverse = reversed


def get(m, *more):
    return m[first(more)] if equal(count(more), 1) else m.get(first(more),
                                                              second(more))


def dissoc(m, *ks):
    return omit(m, ks)


def assoc(m, k, v):
    return set_in(m, (k,), v)


def keys(m):
    return tuple(m.keys())


def slurp(path):
    with open(path) as f:
        return f.read()


zero_ = partial(equal, 0)
empty_ = comp(zero_,
              count)


def take_nth(n, coll):
    return coll[::n]


def array_map(*more):
    return dict(zip(*map(partial(take_nth, 2), (more, tuple(rest(more))))))