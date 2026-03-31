abstract class Item {
    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;

    public Item(String itemId, String name, String description, double startingPrice) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public abstract void printInfo();


    public double getStartingPrice() {
        return startingPrice;
    }
}