public class Seller extends User {

    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String displayRole() {
        return "Seller";
    }
    public Item createItem(String type) {
        System.out.println("Seller " + username + " is creating a " + type);
        return null;
    }
    public void manageItem(Item item) {
        System.out.println("Seller " + username + " is managing item: " + item);
    }
}