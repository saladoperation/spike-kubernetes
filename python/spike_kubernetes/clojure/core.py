import builtins
import functools
import operator
import funcy
from funcy import *

drop_last = butlast


def apply(f, *more):
    return f(*drop_last(more), *last(more))


def _get_items(coll):
    return coll.items() if isinstance(coll, dict) else coll


comp = compose


def map(f, *colls):
    return builtins.map(f, *builtins.map(_get_items, colls))


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
    return functools.reduce(f, _get_items(last(more)), *() if
    equal(count(more), 1) else
    (first(more),))


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


def _flip(f):
    def g(x, *more):
        def h(y, *more_):
            return apply(f, y, x, more_)
        return h if empty_(more) else apply(f, first(more), x, rest(more))
    return g


map_ = partial(_flip(isinstance), dict)
repeat = _flip(funcy.repeat)
zipmap = comp(partial(apply, merge),
              partial(map, array_map))


def select_keys(m, ks):
    return funcy.select_keys(partial(contains_, ks), m)


def filter(f, coll):
    return builtins.filter(f, _get_items(coll))


minus = comp(partial(apply, (partial(reduce, operator.sub))),
             juxt(first, rest),
             vector)
