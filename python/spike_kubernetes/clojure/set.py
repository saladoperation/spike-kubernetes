from spike_kubernetes.clojure.core import *


def make_add_entry(m):
    def add_entry(m_, ks):
        return assoc(m_,
                     second(ks),
                     get(m, first(ks))) if contains_(m, first(ks)) else m_
    return add_entry


def rename_keys(m, key_map):
    return reduce(make_add_entry(m), apply(dissoc, m, keys(key_map)), key_map)
