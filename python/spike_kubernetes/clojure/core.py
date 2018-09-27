import builtins
import functools
import itertools
import numbers
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


map_ = is_mapping


def repeat(n, x):
    return funcy.repeat(x, n)


zipmap = comp(partial(apply, merge),
              partial(map, array_map))


def select_keys(m, ks):
    return funcy.select_keys(partial(contains_, ks), m)


def filter(f, coll):
    return builtins.filter(f, _get_items(coll))


minus = comp(partial(apply, (partial(reduce, operator.sub))),
             juxt(first, rest),
             vector)


def update(m, k, f):
    return update_in(m, [k], f)


range = itertools.count
mapcat = comp(partial(apply, concat),
              map)
plus = comp(partial(reduce, operator.add, 0),
            vector)
repeatedly = comp(partial(apply, funcy.repeatedly),
                  reverse,
                  vector)
multiply = comp(partial(reduce, operator.mul, 1),
                vector)
key = first
val = second


def merge_with(f, *maps):
    def merge_entry(m, e):
        return assoc(m, key(e), f(m[key(e)], val(e)) if
        contains_(m, key(e)) else
        val(e))
    return reduce(partial(reduce, merge_entry), maps)


true_ = partial(equal, True)
every_ = comp(empty_,
              partial(remove, true_),
              map)
mod = comp(second,
           divmod)
vector_ = is_tuple


def conj_(coll, v):
    return assoc(coll, first(v), last(v))


def conj(coll, *more):
    return reduce(conj_, coll, more)


def into(to, from_):
    return reduce(conj, to, from_)


def or_(*more):
    return reduce(operator.or_, more)


def number_(x):
    return isinstance(x, numbers.Number)


string_ = comp(partial(equal, builtins.str),
               type)
