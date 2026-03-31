public abstract class Item implements Entity {
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
    
    @Override
    public String getId() {
        return itemId;
    }

    
    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    
    public abstract void printInfo();
}
