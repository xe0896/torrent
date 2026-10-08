package bencode.structure;

public sealed interface BValue permits BInt, BBytes, BList, BDict {

}
