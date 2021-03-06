package com.fpinjava.laziness.exercise07_08;


import com.fpinjava.common.Function;
import com.fpinjava.common.List;
import com.fpinjava.common.Option;
import com.fpinjava.common.Supplier;
import com.fpinjava.common.TailCall;

import static com.fpinjava.common.TailCall.*;

public abstract class CopyOfStream<T> {

  @SuppressWarnings("rawtypes")
  private static CopyOfStream EMPTY = new Empty();

  public abstract T head();
  public abstract Supplier<CopyOfStream<T>> tail();
  public abstract boolean isEmpty();
  public abstract Option<T> headOption();
  protected abstract Supplier<T> headS();
  public abstract Boolean exists(Function<T, Boolean> p);
  public abstract <U> U foldRight(Supplier<U> z, Function<T, Function<Supplier<U>, U>> f);
  
  private CopyOfStream() {}
  
  public String toString() {
    return toList().toString();
  }
  
  public List<T> toList() {
    return toListIterative();
  }
  
  @SuppressWarnings("unused")
  private TailCall<List<T>> toListRecursive(CopyOfStream<T> s, List<T> acc) {
    return s instanceof Empty
        ? ret(acc)
        : sus(() -> toListRecursive(s.tail().get(), List.cons(s.head(), acc)));
  }

  public List<T> toListIterative() {
    java.util.List<T> result = new java.util.ArrayList<>();
    CopyOfStream<T> ws = this;
    while (!ws.isEmpty()) {
      result.add(ws.head());
      ws = ws.tail().get();
    }
    return List.fromCollection(result);
  }
  
  public CopyOfStream<T> take(Integer n) {
    return n <= 0
        ? CopyOfStream.empty()
        : CopyOfStream.cons(headS(), () -> tail().get().take(n - 1));
  }
  
  public CopyOfStream<T> drop(int n) {
    return n <= 0
        ? this
        : tail().get().drop(n - 1);
  }
  
  public CopyOfStream<T> takeWhile(Function<T, Boolean> p) {
    return isEmpty()
        ? this
        : p.apply(head()) 
            ? cons(headS(), () -> tail().get().takeWhile(p))
            : empty();
  }

  public Boolean existsViaFoldRight(Function<T, Boolean> p) {
    return foldRight(() -> false, a -> b -> p.apply(a) || b.get());
  }
  
  public Boolean forAll(Function<T, Boolean> p) {
    return foldRight(() -> true, a -> b -> p.apply(a) && b.get());
  }

  public CopyOfStream<T> takeWhileViaFoldRight(Function<T, Boolean> p) {
    return foldRight(CopyOfStream::<T> empty, t -> st -> p.apply(t) 
        ? cons(() -> t, () -> st.get())
        : CopyOfStream.<T> empty());
  }
  
  public Option<T> headOptionViaFoldRight() {
    return foldRight(() -> Option.<T>none(), t -> st -> Option.some(t));
  }
  
  public <U> CopyOfStream<U> map(Function<T, U> f) {
    return foldRight(CopyOfStream::<U> empty, t -> su -> cons(() -> f.apply(t), () -> su.get()));
  }
  
  public CopyOfStream<T> filter(Function<T, Boolean> p) {
    return foldRight(CopyOfStream::<T> empty, t -> st -> (p.apply(t))
        ? cons(() -> t, () -> st.get())
        : st.get());
  }

  public CopyOfStream<T> append(CopyOfStream<T> s) {
    return foldRight(() -> s, t -> st -> cons(() -> t, () -> st.get()));
  }

  public <U> CopyOfStream<U> flatMap(Function<T, CopyOfStream<U>> f) {
    return foldRight(CopyOfStream::<U> empty, t -> su -> f.apply(t).append(su.get()));
  }

  public Option<T> find__(Function<T, Boolean> p) {
    Option<T> option = headOptionViaFoldRight();
    return option.isSome() ? option : tail().get().headOptionViaFoldRight();
  }
  
  public Option<T> find_(Function<T, Boolean> p) {
    return this.isEmpty()
        ? Option.none()
        : p.apply(head())
            ? Option.some(head())
            : tail().get().find_(p);
  }
  
  public Option<T> find(Function<T, Boolean> p) {
    return filter(p).headOptionViaFoldRight();
  }

  public static class Empty<T> extends CopyOfStream<T> {

    private Empty() {
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public T head() {
      throw new IllegalStateException("head called on Empty stream");
    }

    @Override
    protected Supplier<T> headS() {
      throw new IllegalStateException("headS called on Empty stream");
    }

    @Override
    public Supplier<CopyOfStream<T>> tail() {
      throw new IllegalStateException("tail called on Empty stream");
    }

    @Override
    public Option<T> headOption() {
      return Option.none();
    }

    @Override
    public Boolean exists(Function<T, Boolean> p) {
      return false;
    }

    @Override
    public <U> U foldRight(Supplier<U> z, Function<T, Function<Supplier<U>, U>> f) {
      return z.get();
    }
  }

  public static class Cons<T> extends CopyOfStream<T> {

    protected final Supplier<T> head;
    
    protected final Supplier<CopyOfStream<T>> tail;

    protected T headM;
    
    private Cons(Supplier<T> head, Supplier<CopyOfStream<T>> tail) {
      this.head = head;
      this.tail = tail;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public T head() {
      if (this.headM == null) {
        this.headM = head.get();
      }
      return this.headM;
    }

    @Override
    protected Supplier<T> headS() {
      return this.head;
    }
    @Override
    public Supplier<CopyOfStream<T>> tail() {
      return this.tail;
    }

    @Override
    public Option<T> headOption() {
      return Option.some(this.head());
    }

    @Override
    public Boolean exists(Function<T, Boolean> p) {
      return p.apply(head()) || tail().get().exists(p);
    }
        
    public <U> U foldRight(Supplier<U> z, Function<T, Function<Supplier<U>, U>> f) { 
      return f.apply(head()).apply(() -> tail().get().foldRight(z, f));
    }
  }

  public static <T> CopyOfStream<T> cons(Supplier<T> hd, Supplier<CopyOfStream<T>> tl) {
    return new Cons<T>(hd, tl);
  }

  public static <T> CopyOfStream<T> cons(Supplier<T> hd, CopyOfStream<T> tl) {
    return new Cons<T>(hd, () -> tl);
  }

  @SuppressWarnings("unchecked")
  public static <T> CopyOfStream<T> empty() {
    return EMPTY;
  }

  public static <T> CopyOfStream<T> cons(List<T> list) {
    return list.isEmpty()
        ? empty()
        : new Cons<T>(() -> list.head(), () -> cons(list.tail()));
  }

  @SafeVarargs
  public static <T> CopyOfStream<T> cons(T... t) {
    return cons(List.list(t));
  }
  
  public static class Head<T> {
    
    private Supplier<T> nonEvaluated;
    private T evaluated;
    
    public Head(Supplier<T> nonEvaluated) {
      super();
      this.nonEvaluated = nonEvaluated;
    }

    public Head(Supplier<T> nonEvaluated, T evaluated) {
      super();
      this.nonEvaluated = nonEvaluated;
      this.evaluated = evaluated;
    }

    public Supplier<T> getNonEvaluated() {
      return nonEvaluated;
    }

    public T getEvaluated() {
      if (evaluated == null) {
        evaluated = nonEvaluated.get();
      }
      return evaluated;
    }
  }

}
