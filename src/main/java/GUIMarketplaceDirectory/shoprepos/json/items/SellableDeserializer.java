/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package GUIMarketplaceDirectory.shoprepos.json.items;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


public class SellableDeserializer extends JsonDeserializer<Sellable> {

    @Override
    public Sellable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode json = p.readValueAsTree();

        try {
            return p.getCodec().treeToValue(json, SellableItemList.class);
        } catch (IOException e) {
            return new CorruptedSellable(json);
        }
    }
}
