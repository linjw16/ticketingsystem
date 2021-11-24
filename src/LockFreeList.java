package ticketingsystem;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList {
    private Node Head = null;

    LockFreeList() {
        this.Head = null;
    }

    public int f_set(int N, Node node) { // Used for initialization.
        if (node != null) {
            this.Head = node;
        }
        return 0;
    }

    public boolean add(Ticket ticket) {
        while (true) {
            Window window = find(Head, ticket.tid);
            Node pred = window.pred, curr = window.curr;
            Node node = new Node(ticket);
            if (pred == null) {
                Head = node;
                return true;
            } else if (curr != null && curr.tkt.tid == ticket.tid) {
                return false;
            } else {
                node.next = new AtomicMarkableReference<Node>(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(Ticket ticket) {
        boolean snip;
        while (true) {
            Window window = find(Head, ticket.tid);
            Node pred = window.pred, curr = window.curr;
            if (pred == null) {
                return false;
            } else if (pred.equals(ticket)) {
                Head = pred.next.getReference();
                return true;
            } else if (curr == null || !curr.equals(ticket)) {
                return false;
            } else {
                Node succ = curr.next.getReference();
                snip = curr.next.compareAndSet(succ, succ, false, true);
                if (!snip) {
                    continue;
                }
                pred.next.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }

    public boolean contains(Ticket item) {
        boolean[] mark = { false };
        Node curr = this.Head;
        while (curr.tkt.tid < item.tid) {
            curr = curr.next.getReference();
        }
        Node succ = curr.next.get(mark);
        return (succ.tkt.tid == item.tid && !mark[0]);

    }

    class Window {
        public Node pred;
        public Node curr;

        Window(Node pred, Node curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    public Window find(Node head, long tid) {
        Node pred = null, curr = null, succ = null;
        boolean[] marked = { false };
        boolean snip;
        retry: while (true) {
            pred = head;
            if (pred == null) {
                return new Window(pred, curr);
            }
            curr = pred.next.getReference();
            while (true) {
                if (curr == null) {
                    return new Window(pred, curr);
                }
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) {
                        continue retry;
                    }
                    curr = succ;
                    if (curr == null) {
                        return new Window(pred, curr);
                    }
                    succ = curr.next.get(marked);
                }
                if (curr.tkt.tid >= tid) {
                    return new Window(pred, curr);
                }
                pred = curr;
                curr = succ;
            }
        }
    }

    public class Node {
        Ticket tkt;
        public AtomicMarkableReference<LockFreeList.Node> next = null;

        Node() {
            this.next = new AtomicMarkableReference<LockFreeList.Node>(null, false);
        }

        Node(Ticket item) {
            this.tkt = item;
            this.next = new AtomicMarkableReference<LockFreeList.Node>(null, false);
        }

        boolean equals(Ticket T) {
            if (tkt.equals(T)) {
                return true;
            } else {
                return false;
            }
        }
    }

}
