package example1;

class Example {

  public static void main(String[] args) {
    A objB = new B();
    A objC = new C();

    new E();

    objB.print(objC);
  }
}

class A extends Object {
  public void print( Object o) { }
}

class B extends A {
  public void print(Object o) { }
}

class C extends B {
  public void print(Object o) { }
}

class D extends A {
  public void print(Object o) { }
}

class E extends A {
  public void print(Object o) { }
}
